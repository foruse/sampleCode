<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace Core\Di;

/**
 * Class Config
 */
class Config
{
    /**
     * Instances of classes
     *
     * @var InstanceClass[]
     */
    private $instances = [];

    /**
     * Constructor
     *
     * @param array $configuration
     * @param Converter $converter
     */
    public function __construct(array $configuration, \Core\Di\Converter $converter)
    {
        $this->instances = $converter->convert($configuration);
    }

    /**
     * Get instance configuration
     *
     * @param string $name
     * @return InstanceClass|null
     */
    public function getInstance($name)
    {
        return isset($this->instances[$name]) ? $this->instances[$name] : null;
    }

    /**
     * Has exist instance
     *
     * @param string $name
     * @return bool
     */
    public function hasInstance($name)
    {
        return isset($this->instances[$name]);
    }
}
