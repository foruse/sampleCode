<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace Core\Di\Converter\Argument\Type;

use Core\Di\Converter\Argument\TypeConverterInterface;

/**
 * Class ArrayType
 */
class ArrayType extends AbstractType implements TypeConverterInterface
{
    const TYPE_ARRAY = 'array';

    /**
     * @param array $data
     * @return array
     */
    public function convert(array $data)
    {
        $result = [];
        $index = 0;
        foreach ($data[self::VALUE] as $name => $item) {

            if (!isset($item['type'])) {
                throw new \InvalidArgumentException('Wrong or not exist type for "' . $name . '" item.');
            }
            $type = $item['type'];

            if ($name === $index) {
                $result[] = $this->typeFactory->create($type)
                    ->convert($item);
                $index += 1;
            } else {
                $result[$name] = $this->typeFactory->create($type)
                    ->convert($item);
            }
        }

        return (array) $result;
    }
}
