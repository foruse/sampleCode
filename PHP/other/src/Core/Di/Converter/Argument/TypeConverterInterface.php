<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace Core\Di\Converter\Argument;

/**
 * Interface TypeConverterInterface
 */
interface TypeConverterInterface
{
    const TYPE = 'type';

    /**
     * @param array $node
     * @return mixed
     */
    public function convert(array $node);
}
